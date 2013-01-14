package org.jetbrains.jet.lang.resolve.java.provider;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.PsiClassFinder;

import static org.jetbrains.jet.lang.resolve.java.provider.DeclarationOrigin.JAVA;
import static org.jetbrains.jet.lang.resolve.java.provider.DeclarationOrigin.KOTLIN;

public class ClassPsiDeclarationProviderImpl extends PsiDeclarationProviderBase implements ClassPsiDeclarationProvider {

    @NotNull
    protected final DeclarationOrigin declarationOrigin;
    @NotNull
    protected final PsiClassFinder psiClassFinder;

    @NotNull
    private final PsiClass psiClass;

    private final boolean staticMembers;

    protected ClassPsiDeclarationProviderImpl(@NotNull PsiClass psiClass, boolean staticMembers, @NotNull PsiClassFinder psiClassFinder) {
        this.staticMembers = staticMembers;
        this.psiClass = psiClass;
        this.psiClassFinder = psiClassFinder;
        this.declarationOrigin = determineOrigin(psiClass);
    }

    @Override
    @NotNull
    protected MembersCache buildMembersCache() {
        return MembersCache.buildMembersByNameCache(new MembersCache(), psiClassFinder, psiClass, null, staticMembers, getDeclarationOrigin() == KOTLIN);
    }

    @Override
    @NotNull
    public PsiClass getPsiClass() {
        return psiClass;
    }

    @Override
    @NotNull
    public DeclarationOrigin getDeclarationOrigin() {
        return declarationOrigin;
    }

    @NotNull
    private static DeclarationOrigin determineOrigin(@Nullable PsiClass psiClass) {
        return ((psiClass != null) && DescriptorResolverUtils.isKotlinClass(psiClass)) ? KOTLIN : JAVA;
    }

    @Override
    public boolean isStaticMembers() {
        return staticMembers;
    }
}
