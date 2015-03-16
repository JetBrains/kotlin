/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.PsiClassStub;
import com.intellij.psi.impl.light.LightTypeParameterListBuilder;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.SmartList;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.utils.KotlinVfsUtil;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class LightClassUtil {
    private static final Logger LOG = Logger.getInstance(LightClassUtil.class);

    public static final File BUILT_INS_SRC_DIR = new File("core/builtins/native", KotlinBuiltIns.BUILT_INS_PACKAGE_NAME.asString());

    private static final class BuiltinsDirUrlHolder {
        private static final URL BUILT_INS_DIR_URL = computeBuiltInsDir();
    }

    /**
     * Checks whether the given file is loaded from the location where Kotlin's built-in classes are defined.
     * As of today, this is core/builtins/native/kotlin directory and files such as Any.kt, Nothing.kt etc.
     *
     * Used to skip JetLightClass creation for built-ins, because built-in classes have no Java counterparts
     */
    public static boolean belongsToKotlinBuiltIns(@NotNull JetFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
            VirtualFile parent = virtualFile.getParent();
            if (parent != null) {
                try {
                    String jetVfsPathUrl = KotlinVfsUtil.convertFromUrl(getBuiltInsDirUrl());
                    String fileDirVfsUrl = parent.getUrl();
                    if (jetVfsPathUrl.equals(fileDirVfsUrl)) {
                        return true;
                    }
                }
                catch (MalformedURLException e) {
                    LOG.error(e);
                }
            }
        }

        // We deliberately return false on error: who knows what weird URLs we might come across out there
        // it would be a pity if no light classes would be created in such cases
        return false;
    }

    @NotNull
    public static URL getBuiltInsDirUrl() {
        return BuiltinsDirUrlHolder.BUILT_INS_DIR_URL;
    }

    @NotNull
    private static URL computeBuiltInsDir() {
        String builtInFilePath = "/" + KotlinBuiltIns.BUILT_INS_PACKAGE_NAME + "/Library.kt";

        URL url = KotlinBuiltIns.class.getResource(builtInFilePath);

        if (url == null) {
            if (ApplicationManager.getApplication().isUnitTestMode()) {
                // HACK: Temp code. Get built-in files from the sources when running from test.
                try {
                    return new URL(StandardFileSystems.FILE_PROTOCOL, "",
                                   FileUtil.toSystemIndependentName(BUILT_INS_SRC_DIR.getAbsolutePath()));
                }
                catch (MalformedURLException e) {
                    throw UtilsPackage.rethrow(e);
                }
            }

            throw new IllegalStateException("Built-ins file wasn't found at url: " + builtInFilePath);
        }

        try {
            return new URL(url.getProtocol(), url.getHost(), PathUtil.getParentPath(url.getFile()));
        }
        catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }

    @Nullable
    /*package*/ static PsiClass findClass(@NotNull FqName fqn, @NotNull StubElement<?> stub) {
        if (stub instanceof PsiClassStub && Comparing.equal(fqn.asString(), ((PsiClassStub) stub).getQualifiedName())) {
            return (PsiClass) stub.getPsi();
        }

        if (stub instanceof PsiClassStub || stub instanceof PsiFileStub) {
            for (StubElement child : stub.getChildrenStubs()) {
                PsiClass answer = findClass(fqn, child);
                if (answer != null) return answer;
            }
        }

        return null;
    }

    @Nullable
    public static PsiClass getPsiClass(@Nullable JetClassOrObject classOrObject) {
        if (classOrObject == null) return null;
        return LightClassGenerationSupport.getInstance(classOrObject.getProject()).getPsiClass(classOrObject);
    }

    @Nullable
    public static PsiMethod getLightClassAccessorMethod(@NotNull JetPropertyAccessor accessor) {
        return getPsiMethodWrapper(accessor);
    }

    @Nullable
    public static PsiField getLightFieldForDefaultObject(@NotNull JetClassOrObject defaultObject) {
        PsiClass outerPsiClass = getWrappingClass(defaultObject);
        if (outerPsiClass != null) {
            for (PsiField fieldOfParent : outerPsiClass.getFields()) {
                if (((KotlinLightElement<?, ?>) fieldOfParent).getOrigin() == defaultObject &&
                    fieldOfParent.getName().equals(defaultObject.getName())) { // TODO this check is relevant while light class has deprecated OBJECT$ field
                    return fieldOfParent;
                }
            }
        }
        return null;
    }

    @NotNull
    public static PropertyAccessorsPsiMethods getLightClassPropertyMethods(@NotNull JetProperty property) {
        JetPropertyAccessor getter = property.getGetter();
        JetPropertyAccessor setter = property.getSetter();

        PsiMethod getterWrapper = getter != null ? getLightClassAccessorMethod(getter) : null;
        PsiMethod setterWrapper = setter != null ? getLightClassAccessorMethod(setter) : null;

        return extractPropertyAccessors(property, getterWrapper, setterWrapper);
    }

    @NotNull
    public static PropertyAccessorsPsiMethods getLightClassPropertyMethods(@NotNull JetParameter parameter) {
        return extractPropertyAccessors(parameter, null, null);
    }

    @Nullable
    public static PsiMethod getLightClassMethod(@NotNull JetNamedFunction function) {
        return getPsiMethodWrapper(function);
    }

    @Nullable
    private static PsiMethod getPsiMethodWrapper(@NotNull JetDeclaration declaration) {
        List<PsiMethod> wrappers = getPsiMethodWrappers(declaration, false);
        return !wrappers.isEmpty() ? wrappers.get(0) : null;
    }

    @NotNull
    private static List<PsiMethod> getPsiMethodWrappers(@NotNull JetDeclaration declaration, boolean collectAll) {
        PsiClass psiClass = getWrappingClass(declaration);
        if (psiClass == null) {
            return Collections.emptyList();
        }

        List<PsiMethod> methods = new SmartList<PsiMethod>();
        for (PsiMethod method : psiClass.getMethods()) {
            try {
                if (method instanceof KotlinLightMethod && ((KotlinLightMethod) method).getOrigin() == declaration) {
                    methods.add(method);
                    if (!collectAll) {
                        return methods;
                    }
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                throw new IllegalStateException(
                        "Error while wrapping declaration " + declaration.getName() +
                        "Context\n:" +
                        String.format("=== In file ===\n" +
                                      "%s\n" +
                                      "=== On element ===\n" +
                                      "%s\n" +
                                      "=== WrappedElement ===\n" +
                                      "%s\n",
                                      declaration.getContainingFile().getText(),
                                      declaration.getText(),
                                      method.toString()),
                        e
                );
            }
        }

        return methods;
    }

    @Nullable
    private static PsiClass getWrappingClass(@NotNull JetDeclaration declaration) {
        if (declaration instanceof JetParameter) {
            JetClass constructorClass = JetPsiUtil.getClassIfParameterIsProperty((JetParameter) declaration);
            if (constructorClass != null) {
                return getPsiClass(constructorClass);
            }
        }

        if (declaration instanceof JetPropertyAccessor) {
            PsiElement propertyParent = declaration.getParent();
            assert propertyParent instanceof JetProperty : "JetProperty is expected to be parent of accessor";

            declaration = (JetProperty) propertyParent;
        }

        //noinspection unchecked
        if (PsiTreeUtil.getParentOfType(declaration, JetFunction.class, JetProperty.class) != null) {
            // Can't get wrappers for internal declarations. Their classes are not generated during calcStub
            // with ClassBuilderMode.LIGHT_CLASSES mode, and this produces "Class not found exception" in getDelegate()
            return null;
        }

        PsiElement parent = declaration.getParent();

        if (parent instanceof JetFile) {
            // top-level declaration
            FqName fqName = PackageClassUtils.getPackageClassFqName(((JetFile) parent).getPackageFqName());
            Project project = declaration.getProject();
            return JavaElementFinder.getInstance(project).findClass(fqName.asString(), GlobalSearchScope.allScope(project));
        }
        else if (parent instanceof JetClassBody) {
            assert parent.getParent() instanceof JetClassOrObject;
            return getPsiClass((JetClassOrObject) parent.getParent());
        }

        return null;
    }

    @NotNull
    private static PropertyAccessorsPsiMethods extractPropertyAccessors(
            @NotNull JetDeclaration jetDeclaration,
            @Nullable PsiMethod specialGetter, @Nullable PsiMethod specialSetter
    ) {
        PsiMethod getterWrapper = specialGetter;
        PsiMethod setterWrapper = specialSetter;

        if (getterWrapper == null || setterWrapper == null) {
            // If some getter or setter isn't found yet try to get it from wrappers for general declaration

            List<PsiMethod> wrappers = KotlinPackage.filter(
                    getPsiMethodWrappers(jetDeclaration, true),
                    new Function1<PsiMethod, Boolean>() {
                        @Override
                        public Boolean invoke(PsiMethod method) {
                            return JvmAbi.isAccessorName(method.getName());
                        }
                    }
            );
            assert wrappers.size() <= 2 : "Maximum two wrappers are expected to be generated for declaration: " + jetDeclaration.getText();

            for (PsiMethod wrapper : wrappers) {
                if (wrapper.getName().startsWith(JvmAbi.SETTER_PREFIX)) {
                    assert setterWrapper == null : String.format(
                            "Setter accessor isn't expected to be reassigned (old: %s, new: %s)", setterWrapper, wrapper);

                    setterWrapper = wrapper;
                }
                else {
                    assert getterWrapper == null : String.format(
                            "Getter accessor isn't expected to be reassigned (old: %s, new: %s)", getterWrapper, wrapper);

                    getterWrapper = wrapper;
                }
            }
        }

        return new PropertyAccessorsPsiMethods(getterWrapper, setterWrapper);
    }

    @NotNull
    public static PsiTypeParameterList buildLightTypeParameterList(
            PsiTypeParameterListOwner owner,
            JetDeclaration declaration) {
        LightTypeParameterListBuilder builder = new KotlinLightTypeParameterListBuilder(owner.getManager());
        if (declaration instanceof JetTypeParameterListOwner) {
            JetTypeParameterListOwner typeParameterListOwner = (JetTypeParameterListOwner) declaration;
            List<JetTypeParameter> parameters = typeParameterListOwner.getTypeParameters();
            for (int i = 0; i < parameters.size(); i++) {
                JetTypeParameter jetTypeParameter = parameters.get(i);
                String name = jetTypeParameter.getName();
                String safeName = name == null ? "__no_name__" : name;
                builder.addParameter(new KotlinLightTypeParameter(owner, i, safeName));
            }
        }
        return builder;
    }

    public static class PropertyAccessorsPsiMethods implements Iterable<PsiMethod> {
        private final PsiMethod getter;
        private final PsiMethod setter;
        private final Collection<PsiMethod> accessors = new ArrayList<PsiMethod>(2);

        PropertyAccessorsPsiMethods(@Nullable PsiMethod getter, @Nullable PsiMethod setter) {
            this.getter = getter;
            if (getter != null) {
                accessors.add(getter);
            }

            this.setter = setter;
            if (setter != null) {
                accessors.add(setter);
            }
        }

        @Nullable
        public PsiMethod getGetter() {
            return getter;
        }

        @Nullable
        public PsiMethod getSetter() {
            return setter;
        }

        @NotNull
        @Override
        public Iterator<PsiMethod> iterator() {
            return accessors.iterator();
        }
    }

    private LightClassUtil() {
    }
}
