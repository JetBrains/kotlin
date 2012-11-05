package org.jetbrains.jet.lang.resolve.java.provider;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;

import static org.jetbrains.jet.lang.resolve.java.provider.Origin.JAVA;
import static org.jetbrains.jet.lang.resolve.java.provider.Origin.KOTLIN;

public class ClassPsiDeclarationProviderImpl extends PsiDeclarationProviderBase implements ClassPsiDeclarationProvider {

    @NotNull
    private final PsiClass psiClass;
    private final boolean staticMembers;
    @NotNull
    protected final Origin origin;

    protected ClassPsiDeclarationProviderImpl(
            @NotNull PsiClass psiClass, boolean staticMembers
    ) {
        this.staticMembers = staticMembers;
        this.psiClass = psiClass;
        this.origin = determineOrigin(psiClass);
    }

    @Override
    @NotNull
    protected MembersCache buildMembersCache() {
        return MembersCache.buildMembersByNameCache(new MembersCache(), psiClass, null, staticMembers, getOrigin() == KOTLIN);
    }

    @Override
    @NotNull
    public PsiClass getPsiClass() {
        return psiClass;
    }

    @Override
    @NotNull
    public Origin getOrigin() {
        return origin;
    }

    @NotNull
    private static Origin determineOrigin(@Nullable PsiClass psiClass) {
        return ((psiClass != null) && DescriptorResolverUtils.isKotlinClass(psiClass)) ? KOTLIN : JAVA;
    }

    @Override
    public boolean isStaticMembers() {
        return staticMembers;
    }
}
