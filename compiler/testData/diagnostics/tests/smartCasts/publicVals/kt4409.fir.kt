// RUN_PIPELINE_TILL: BACKEND
public interface A {
    public val x: Any
}

public class B(override public val x: Any) : A {
    fun foo(): Int {
        if (x is String) {
            return x.length
        } else {
            return 0
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, integerLiteral, interfaceDeclaration,
isExpression, override, primaryConstructor, propertyDeclaration, smartcast */
