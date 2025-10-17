// RUN_PIPELINE_TILL: FRONTEND
class Xyz {
    fun x(): String? {
        return <!RETURN_TYPE_MISMATCH!>try {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[<!UNRESOLVED_REFERENCE!>a<!>]<!> <!USELESS_ELVIS!>?: <!UNRESOLVED_REFERENCE!>XYZ<!><!>
        }
        catch (e: Exception) {
            null
        }<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, elvisExpression, functionDeclaration, localProperty,
nullableType, propertyDeclaration, tryExpression */
