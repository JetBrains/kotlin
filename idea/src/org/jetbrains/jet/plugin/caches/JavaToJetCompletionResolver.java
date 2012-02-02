package org.jetbrains.jet.plugin.caches;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.xml.impl.schema.TypeDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Get jet declarations from java that could be used in completion. Unlike the real jet resolver this helper is allowed
 * to return partially unresolved descriptors in exchange of execution speed.
 *
 * @author Nikolay Krasko
 */
class JetFromJavaDescriptorHelper {

    private JetFromJavaDescriptorHelper() {
    }

    /**
     * Get java equivalents for jet top level classes.
     */
    static PsiClass[] getClassesForJetNamespaces(Project project, GlobalSearchScope scope) {
        return PsiShortNamesCache.getInstance(project).getClassesByName(JvmAbi.PACKAGE_CLASS, scope);
    }

    /**
     * Get names that could have jet descriptor equivalents. It could be inaccurate and return more results than necessary.
     */
    static Collection<String> getPossiblePackageDeclarationsNames(Project project, GlobalSearchScope scope) {
        final ArrayList<String> result = new ArrayList<String>();

        for (PsiClass jetNamespaceClass : getClassesForJetNamespaces(project, scope)) {
            for (PsiMethod psiMethod : jetNamespaceClass.getMethods()) {
                if (psiMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                    result.add(psiMethod.getName());
                }
            }
        }

        return result;
    }
    
    static Collection<String> getTopExtensionFunctionNames(TypeDescriptor typeDescriptor, Project project,
                                                           GlobalSearchScope scope) {

        return getPossiblePackageDeclarationsNames(project, scope);
    }

}
