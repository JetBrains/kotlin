// FILE: InOut.kt
interface In<in T>

// FILE: J1.java
public interface J1 {
    In<String> foo();
}

// FILE: J2.java
import org.jetbrains.annotations.*;

public interface J2 {
    @NotNull In<String> foo();
}

// FILE: J3.java
import org.jetbrains.annotations.*;

public interface J3 {
    @Nullable In<String> foo();
}

// FILE: K.kt
interface K1 {
    fun foo(): In<String>
}

interface K2 {
    fun foo(): In<String?>
}

// FIXME TestJ1K1 should have foo(): In<String!>, since In<String!> <: In<String>.
interface TestJ1K1 : J1, K1
interface TestK1J1 : K1, J1

interface TestJ1K2 : J1, K2
interface TestK2J1 : K2, J1

interface TestJ2K1 : J2, K1
interface TestK1J2 : K1, J2

interface TestJ2K2 : J2, K2
interface TestK2J2 : K2, J2

interface TestJ3K1 : J3, K1
interface TestK1J3 : K1, J3

interface TestJ3K2 : J3, K2
interface TestK2J3 : K2, J3

interface TestJ1K1K2 : J1, K1, K2
interface TestK1J1K2 : K1, J1, K2
interface TestK1K2J1 : K1, K2, J1

interface TestJ2K1K2 : J2, K1, K2
interface TestK1J2K2 : K1, J2, K2
interface TestK1K2J2 : K1, K2, J2

interface TestJ3K1K2 : J3, K1, K2
interface TestK1J3K2 : K1, J3, K2
interface TestK1K2J3 : K1, K2, J3