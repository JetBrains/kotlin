// IS_APPLICABLE: false
public trait I {
    public val v: String?
}

public trait I1 : I {
    override val v: String<caret>
}