/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.codegen.binding;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.AsmUtil;
import org.jetbrains.jet.codegen.ClassBuilderFactories;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.PackagePartClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils.descriptorToDeclaration;

public final class PsiCodegenPredictor {
    private PsiCodegenPredictor() {
    }

    public static boolean checkPredictedNameFromPsi(@NotNull DeclarationDescriptor descriptor, @Nullable Type nameFromDescriptors) {
        PsiElement element = descriptorToDeclaration(descriptor);
        if (element instanceof JetDeclaration) {
            String classNameFromPsi = getPredefinedJvmInternalName((JetDeclaration) element);
            assert classNameFromPsi == null || Type.getObjectType(classNameFromPsi).equals(nameFromDescriptors) :
                    String.format("Invalid algorithm for getting qualified name from psi! Predicted: %s, actual %s\n" +
                                  "Element: %s", classNameFromPsi, nameFromDescriptors, element.getText());
        }

        return true;
    }

    /**
     * @return null if no prediction can be done.
     */
    @Nullable
    public static String getPredefinedJvmInternalName(@NotNull JetDeclaration declaration) {
        // TODO: Method won't work for declarations inside class objects
        // TODO: Method won't give correct class name for traits implementations

        JetDeclaration parentDeclaration = JetStubbedPsiUtil.getContainingDeclaration(declaration);
        if (parentDeclaration instanceof JetClassObject) {
            assert declaration instanceof JetObjectDeclaration : "Only object declarations can be children of JetClassObject: " + declaration;
            return getPredefinedJvmInternalName(parentDeclaration);
        }

        String parentInternalName;
        if (parentDeclaration != null) {
            parentInternalName = getPredefinedJvmInternalName(parentDeclaration);
            if (parentInternalName == null) {
                return null;
            }
        }
        else {
            FqName packageFqName = declaration.getContainingJetFile().getPackageFqName();

            if (declaration instanceof JetNamedFunction) {
                Name name = ((JetNamedFunction) declaration).getNameAsName();
                return name == null ? null : PackageClassUtils.getPackageClassInternalName(packageFqName) + "$" + name.asString();
            }

            parentInternalName = AsmUtil.internalNameByFqNameWithoutInnerClasses(packageFqName);
        }

        if (declaration instanceof JetClassObject) {
            // Get parent and assign Class object prefix
            return parentInternalName + JvmAbi.CLASS_OBJECT_SUFFIX;
        }

        if (!PsiTreeUtil.instanceOf(declaration, JetClass.class, JetObjectDeclaration.class, JetNamedFunction.class, JetProperty.class) ||
                declaration instanceof JetEnumEntry) {
            // Other subclasses are not valid for class name prediction.
            // For example EnumEntry, JetFunctionLiteral
            return null;
        }

        Name name = ((JetNamedDeclaration) declaration).getNameAsName();
        if (name == null) {
            return null;
        }

        if (declaration instanceof JetNamedFunction) {
            if (!(parentDeclaration instanceof JetClass || parentDeclaration instanceof JetObjectDeclaration)) {
                // Can't generate predefined name for internal functions
                return null;
            }
        }

        // NOTE: looks like a bug - for class in getter of top level property class name will be $propertyName$ClassName but not
        // PackageClassName$propertyName$ClassName
        if (declaration instanceof JetProperty) {
            return parentInternalName + "$" + name.asString();
        }

        if (parentInternalName.isEmpty()) {
            return name.asString();
        }

        return parentInternalName + (parentDeclaration == null ? "/" : "$") + name.asString();
    }

    @Nullable
    public static JetFile getFileForPackagePartName(@NotNull Collection<JetFile> allPackageFiles, @NotNull JvmClassName className) {
        for (JetFile file : allPackageFiles) {
            String internalName = PackagePartClassUtils.getPackagePartInternalName(file);
            JvmClassName jvmClassName = JvmClassName.byInternalName(internalName);
            if (jvmClassName.equals(className)) {
                return file;
            }
        }
        return null;
    }

    @Nullable
    public static JetFile getFileForCodegenNamedClass(
            @NotNull ModuleDescriptor module,
            @NotNull BindingContext context,
            @NotNull Collection<JetFile> allPackageFiles,
            @NotNull String classInternalName
    ) {
        Project project = allPackageFiles.iterator().next().getProject();
        GenerationState state = new GenerationState(project, ClassBuilderFactories.THROW_EXCEPTION, module, context,
                                                    new ArrayList<JetFile>(allPackageFiles));
        state.beforeCompile();

        BindingTrace trace = state.getBindingTrace();
        for (ClassDescriptor classDescriptor : trace.getKeys(CodegenBinding.ASM_TYPE)) {
            Type type = trace.get(CodegenBinding.ASM_TYPE, classDescriptor);
            if (type != null && classInternalName.equals(type.getInternalName())) {
                return DescriptorToSourceUtils.getContainingFile(classDescriptor);
            }
        }

        return null;
    }
}
