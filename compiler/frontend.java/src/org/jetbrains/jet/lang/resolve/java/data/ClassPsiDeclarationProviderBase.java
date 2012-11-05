package org.jetbrains.jet.lang.resolve.java.data;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.MembersCache;

import static org.jetbrains.jet.lang.resolve.java.data.Origin.JAVA;
import static org.jetbrains.jet.lang.resolve.java.data.Origin.KOTLIN;

public abstract class ClassPsiDeclarationProviderBase extends PsiDeclarationProviderBase implements ClassPsiDeclarationProvider {

    @NotNull
    private final PsiClass psiClass;
    private final boolean staticMembers;
    @NotNull
    protected final Origin origin;

    public ClassPsiDeclarationProviderBase(
            boolean staticMembers,
            @NotNull PsiClass psiClass
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
    public boolean isEmpty() {
        return false;
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

    public boolean isStaticMembers() {
        return staticMembers;
    }
}
