import kotlin.test.assertEquals

fun foo(value: Boolean): Int {
    if (value)
        return 1
    else
        return 2
}

fun box(): String {
    assertEquals(1, foo(true))
    assertEquals(2, foo(false))
    
    return "OK"
}

/*
    3 return's are defined in functions
*/

// 2 IRETURN
// 1 ARETURN
