package test

public class B {

    public fun test(): String {
        TopLevelKt() // error here
        return TopLevelKt.foo("OK") // no error here: can still call static functions
    }

}