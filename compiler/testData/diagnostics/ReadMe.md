Several directives can be added in the beginning of a test file in the syntax:

`// !DIRECTIVE`

## Directives:

### 1. DIAGNOSTICS

Must be

    '([ + - ! ] DIAGNOSTIC_FACTORY_NAME | ERROR | WARNING | INFO ) +'

  where

* `'+'` means 'include';
* `'-'` means 'exclude';
* `'!'` means 'exclude everything but this'.

  Directives are applied in the order of appearance,
  i.e. `!FOO +BAR` means include only `FOO` and `BAR`.

#### Examples:

    // !DIAGNOSTICS: -WARNING +CAST_NEVER_SUCCEEDS

    // !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE


### 2. CHECK_TYPE

The directive adds the following declarations to the file:

    class _<T>
    fun <T> T.checkType(f: (_<T>) -> Unit) = f

With that, an exact type of an expression can be checked in the following way:

    fun test(expr: A) {
       expr checkType { it: _<A> }
    }

#### Usage:

    // !CHECK_TYPE

### 3. FILE

The directive let you compose a test consisting of several files in one actual file.

#### Usage:
// FILE: A.java
/* Java code */

// FILE: B.kt
/* kotlin code */