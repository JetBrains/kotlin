// FILE: J.java
public interface J {
    java.util.List<String> foo();
}

// FILE: K.kt
interface ILNS {
    fun foo(): List<String?>
}

interface IMLS {
    fun foo(): MutableList<String>
}

interface IMLNS {
    fun foo(): MutableList<String?>
}

interface ILS {
    fun foo(): List<String>
}

interface Test1 : ILNS, J
interface Test2 : J, ILNS

interface Test3 : IMLS, J
interface Test4 : J, IMLS

interface Test5 : ILNS, IMLS, J
interface Test6 : ILNS, J, IMLS
interface Test7 : J, ILNS, IMLS

// ILS and IMLNS are incompatible

<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>interface Test8<!> : J, IMLNS, ILS
<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>interface Test9<!> : IMLNS, J, ILS
<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>interface Test10<!> : IMLNS, ILS, J