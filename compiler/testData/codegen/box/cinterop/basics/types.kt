// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: ctypes.def
---

// KT-28065
struct StructWithConstFields {
    int x;
    const int y;
};

struct StructWithConstFields getStructWithConstFields() {
    struct StructWithConstFields result = { 111, 222 };
    return result;
}

enum ForwardDeclaredEnum;
enum ForwardDeclaredEnum {
    ZERO, ONE, TWO
};

static int vlaSum(int size, int array[size]) {
    int result = 0;
    for (int i = 0; i < size; ++i) {
        result += array[i];
    }
    return result;
}

static int vlaSum2D(int size, int array[][size]) {
    int result = 0;
    for (int i = 0; i < size; ++i) {
        for (int j = 0; j < size; ++j) {
            result += array[i][j];
        }
    }
    return result;
}

static int vlaSum2DBothDimensions(int rows, int columns, int array[rows][columns]) {
    int result = 0;
    for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < columns; ++j) {
        result += array[i][j];
    }
    }
    return result;
}

/*
// Not supported by clang:
static int vlaSum2DForward(int size; int array[][size], int size) {
    return vlaSum2D(size, array);
}
*/

// "Strict" enums heuristic based on whether enum constants are defined explicitly:
enum StrictEnum1 {
    StrictEnum1A,
    StrictEnum1B
};

enum StrictEnum2 {
    StrictEnum2A __attribute__((unused)),
    StrictEnum2B __attribute__((unused))
};

enum NonStrictEnum1 {
    NonStrictEnum1A = 0,
    NonStrictEnum1B __attribute__((unused))
};

enum NonStrictEnum2 {
    NonStrictEnum2A,
    NonStrictEnum2B __attribute__((unused)) = 1
};

// KT-34025
typedef char Char;
enum EnumCharBase : Char {
    EnumCharBaseA,
    EnumCharBaseB
};

static int sendEnum(enum EnumCharBase x) {
    return (int)x + 2;
}

enum EnumExplicitChar : char {
    EnumExplicitCharA = 'a',
    EnumExplicitCharB = 'b',
    EnumExplicitCharDup = 'a'
};

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import kotlin.native.*
import kotlin.test.*
import ctypes.*

fun box(): String {
    getStructWithConstFields().useContents {
        assertEquals(111, x)
        assertEquals(222, y)
    }

    assertEquals(1u, ForwardDeclaredEnum.ONE.value)

    assertEquals(6, vlaSum(3, cValuesOf(1, 2, 3)))
    assertEquals(10, vlaSum2D(2, cValuesOf(1, 2, 3, 4)))
    assertEquals(21, vlaSum2DBothDimensions(2, 3, cValuesOf(1, 2, 3, 4, 5, 6)))

    // Not supported by clang:
    // assertEquals(10, vlaSum2DForward(cValuesOf(1, 2, 3, 4), 2))

    assertEquals(0u, StrictEnum1.StrictEnum1A.value)
    assertEquals(1u, StrictEnum2.StrictEnum2B.value)
    assertEquals(0u, NonStrictEnum1A)
    assertEquals(1u, NonStrictEnum2B)
    assertEquals(1, EnumCharBase.EnumCharBaseB.value)
    assertEquals(3, sendEnum(EnumCharBase.EnumCharBaseB))
    assertEquals('a'.toByte(), EnumExplicitCharA)
    assertEquals('b'.toByte(), EnumExplicitCharB)
    assertEquals(EnumExplicitCharA, EnumExplicitCharDup)

    return "OK"
}

