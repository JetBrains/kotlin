// "Remove explicitly specified return type to match overridden method" "true"
abstract class A : java.util.Iterator<Int> {
    public abstract override fun remove() : Int<caret>;
}
