// FIR_IDENTICAL
// ISSUE: KT-57166

// FILE: PsiType.java

public abstract class PsiType {
}

// FILE: JavaCodeFragment.java

public interface JavaCodeFragment {
    PsiType getThisType();
    void setThisType(PsiType psiType);
}

// FILE: KtCodeFragment.kt

abstract class KtCodeFragment : JavaCodeFragment {
    private var thisType: PsiType? = null

    override fun getThisType() = thisType

    override fun setThisType(psiType: PsiType?) {
        thisType = psiType
    }
}
