public class X {
    public var x : String? = null
    private var y: String? = "abc"
    public fun fn(): Int {
        if (x != null)
            // Smartcast is not possible for variable properties
            return x.<!INAPPLICABLE_CANDIDATE!>length<!>
        else if (y != null)
            // Even if they are private
            return y.<!INAPPLICABLE_CANDIDATE!>length<!>
        else
            return 0
    }
}

