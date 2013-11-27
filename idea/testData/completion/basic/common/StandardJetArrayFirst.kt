fun main(args: Array<String>) {
    Array<caret>
}

// INVOCATION_COUNT: 2
// WITH_ORDER: 1
// EXIST: { lookupString:"Array", tailText:" (jet)" }
// EXIST_JAVA_ONLY: { lookupString:"Array", tailText:" (java.sql)" }
// EXIST_JAVA_ONLY: { lookupString:"ArrayList", tailText:"<E> (java.util)" }
// EXIST_JS_ONLY: { lookupString:"ArrayList", tailText:" (java.util)" }





// Developer! Every ancient failure of this test is important!