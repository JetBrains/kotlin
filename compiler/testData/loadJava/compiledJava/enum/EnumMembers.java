package test;

public enum EnumMembers {
    FIRST(true),
    SECOND(false);

    public final boolean isFirst;

    private EnumMembers(boolean isFirst) {
        this.isFirst = isFirst;
    }

    public boolean first() {
        return isFirst;
    }
}
