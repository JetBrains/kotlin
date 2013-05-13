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

package org.jetbrains.jet.asJava;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.java.stubs.PsiClassStub;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.binding.PsiCodegenPredictor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.JetClsMethod;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.utils.KotlinVfsUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class LightClassUtil {
    private static final Logger LOG = Logger.getInstance(LightClassUtil.class);
    private static final String DEFINITION_OF_ANY = "Any.jet";

    /**
     * Checks whether the given file is loaded from the location where Kotlin's built-in classes are defined.
     * As of today, this is compiler/frontend/src/jet directory and files such as Any.jet, Nothing.jet etc.
     *
     * Used to skip JetLightClass creation for built-ins, because built-in classes have no Java counterparts
     */
    public static boolean belongsToKotlinBuiltIns(@NotNull JetFile file) {
        try {
            String jetVfsPathUrl = KotlinVfsUtil.convertFromUrl(getBuiltInsDirResourceUrl());
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                VirtualFile parent = virtualFile.getParent();
                if (parent != null) {
                    String fileDirVfsUrl = parent.getUrl() + "/" + DEFINITION_OF_ANY;
                    if (jetVfsPathUrl.equals(fileDirVfsUrl)) {
                        return true;
                    }
                }
            }
        }
        catch (MalformedURLException e) {
            LOG.error(e);
        }
        // We deliberately return false on error: who knows what weird URLs we might come across out there
        // it would be a pity if no light classes would be created in such cases
        return false;
    }

    @NotNull
    public static URL getBuiltInsDirResourceUrl() {
        String pathToAny = "/" + KotlinBuiltIns.BUILT_INS_DIR + "/" + DEFINITION_OF_ANY;
        URL url = KotlinBuiltIns.class.getResource(pathToAny);
        if (url == null) {
            throw new IllegalStateException("Built-ins not found in the classpath: " + pathToAny);
        }
        return url;
    }

    /*package*/ static void logErrorWithOSInfo(@Nullable Throwable cause, @NotNull FqName fqName, @Nullable VirtualFile virtualFile) {
        String path = virtualFile == null ? "<null>" : virtualFile.getPath();
        LOG.error(
                "Could not generate LightClass for " + fqName + " declared in " + path + "\n" +
                "built-ins dir URL is " + getBuiltInsDirResourceUrl() + "\n" +
                "System: " + SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION + " Java Runtime: " + SystemInfo.JAVA_RUNTIME_VERSION,
                cause);
    }

    @Nullable
    /*package*/ static PsiClass findClass(@NotNull FqName fqn, @NotNull StubElement<?> stub) {
        if (stub instanceof PsiClassStub && Comparing.equal(fqn.getFqName(), ((PsiClassStub) stub).getQualifiedName())) {
            return (PsiClass)stub.getPsi();
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

    @NotNull
    public static PropertyAccessorsPsiMethods getLightClassPropertyMethods(@NotNull JetProperty property) {
        JetPropertyAccessor getter = property.getGetter();
        JetPropertyAccessor setter = property.getSetter();

        PsiMethod getterWrapper = getter != null ? getLightClassAccessorMethod(getter) : null;
        PsiMethod setterWrapper = setter != null ? getLightClassAccessorMethod(setter) : null;

        return extractPropertyAccessors(property, getterWrapper, setterWrapper);
    }

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
                if (method instanceof JetClsMethod) {
                    if (method instanceof PsiCompiledElement && ((PsiCompiledElement) method).getMirror() == declaration) {
                        methods.add(method);
                        if (!collectAll) {
                            return methods;
                        }
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
        if (declaration instanceof JetParameter && declaration.getParent().getParent() instanceof JetClass) {
            return getPsiClass((JetClassOrObject) declaration.getParent().getParent());
        }

        if (declaration instanceof JetPropertyAccessor) {
            PsiElement propertyParent = declaration.getParent();
            assert propertyParent instanceof JetProperty : "JetProperty is expected to be parent of accessor";

            declaration = (JetProperty) propertyParent;
        }

        //noinspection unchecked
        if (PsiTreeUtil.getParentOfType(declaration, JetFunction.class, JetProperty.class) != null) {
            // Can't get wrappers for internal declarations. Their classes are not generated during calcStub
            // with ClassBuilderMode.SIGNATURES mode, and this produces "Class not found exception" in getDelegate()
            return null;
        }

        PsiElement parent = declaration.getParent();

        if (parent instanceof JetFile) {
            // top-level declaration
            JvmClassName jvmName = PsiCodegenPredictor.getPredefinedJvmClassName((JetFile) parent, true);
            if (jvmName != null) {
                Project project = declaration.getProject();

                String fqName = jvmName.getFqName().getFqName();
                return JavaElementFinder.getInstance(project).findClass(fqName, GlobalSearchScope.allScope(project));
            }
        }
        else if (parent instanceof JetClassBody) {
            assert parent.getParent() instanceof JetClassOrObject;
            return getPsiClass((JetClassOrObject) parent.getParent());
        }

        return null;
    }

    private static PropertyAccessorsPsiMethods extractPropertyAccessors(
            @NotNull JetDeclaration jetDeclaration,
            @Nullable PsiMethod specialGetter, @Nullable PsiMethod specialSetter)
    {
        PsiMethod getterWrapper = specialGetter;
        PsiMethod setterWrapper = specialSetter;

        if (getterWrapper == null || setterWrapper == null) {
            // If some getter or setter isn't found yet try to get it from wrappers for general declaration

            List<PsiMethod> wrappers = getPsiMethodWrappers(jetDeclaration, true);
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

    private LightClassUtil() {}
}
