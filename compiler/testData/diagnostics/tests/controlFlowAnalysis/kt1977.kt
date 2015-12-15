package kt1977

//KT-1977 Wrong 'unused expression' in catch
fun strToInt(s : String) : Int? =
try {
    Integer.parseInt(s)
} catch(e : NumberFormatException) {
    null
}

//more tests
fun test1(s : String) : Int? {
    return try {
        <!UNUSED_EXPRESSION!>88<!>
        Integer.parseInt(s)
        22
    }
    catch (e: NumberFormatException) {
        44
    }
    finally {
        <!UNUSED_EXPRESSION!>22<!>
    }
}

fun test2(s : String) : Int? {
    return try {
        <!UNUSED_EXPRESSION!>88<!>
        Integer.parseInt(s)
        22
    } finally {
            <!UNUSED_LAMBDA_EXPRESSION!>{
            x : Int -> x
            }<!>
    }
}


//KT-2015 False "Expression is unused" warnings
fun foo() {
    val <!UNUSED_VARIABLE!>i<!> : Int = try{
        bar()
        1
    }
    catch(e : Exception){
        0
    }
}

fun bar() {
}