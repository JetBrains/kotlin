public open class X {
    protected val x : String? = null
    public fun fn(): Int {
        if (x != null)
            // Smartcast is possible for protected value property in the same class
            return <!DEBUG_INFO_SMARTCAST!>x<!>.length
        else
            return 0
    }
}

public class Y: X() {
    public fun bar(): Int {
        // Smartcast is possible even in derived class
        return if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.length else 0
    }
}
