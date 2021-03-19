interface PsiMember

interface PsiField : PsiMember

abstract class LightMemberImpl<out D : PsiMember>(computeRealDelegate: () -> D) {
    open val delegate by lazy(computeRealDelegate)
}

abstract class LightFieldImpl<D : PsiField>(computeRealDelegate: () -> D) : LightMemberImpl<PsiField>(computeRealDelegate) {
    override val delegate: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>D<!>
        get() = super.delegate
}