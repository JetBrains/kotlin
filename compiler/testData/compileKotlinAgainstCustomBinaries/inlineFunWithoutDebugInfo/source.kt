package test

public class B {

    public fun test(): String {
        var p = "fail"
        A().test {
            p = "OK"
        }
        return p
    }

}