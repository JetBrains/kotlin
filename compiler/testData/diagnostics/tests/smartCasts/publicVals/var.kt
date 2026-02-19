// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
public class X {
    public var x : String? = null
    private var y: String? = "abc"
    public fun fn(): Int {
        if (x != null)
            // Smartcast is not possible for variable properties
            return <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        else if (y != null)
            // Even if they are private
            return <!SMARTCAST_IMPOSSIBLE!>y<!>.length
        else
            return 0
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
nullableType, propertyDeclaration, smartcast, stringLiteral */
