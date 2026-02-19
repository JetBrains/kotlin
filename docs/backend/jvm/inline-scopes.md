## Introduction
The Kotlin debugger uses inline marker variables (`$i$f$...`, `$i$a$...`) that are produced by the Kotlin compiler 
to handle stepping into/over/out of inline functions and lambdas and also to build inline stack traces. This scheme 
does not work for dex code, where the same variable may be duplicated in different registers and where basic blocks can be reordered. 
This leads to many debugging issues on Android from inconsistent locals in the variables view to bad stepping in inline functions. 

## **New format description**
The new format affects 3 types of variables:
1. Inline function marker variables
   `$i$f$name\[scope number]\[call site line number]`
2. Inline lambda marker variables
   `$i$a$name\[scope number]\[call site line number]\[surrounding scope number]`
3. Local variables from inline functions or lambdas
   `name\[scope number]`

Let’s break it down on the following example:
```
fun main() {
  val inMain = 0
  g(0) {
    val inLambdaG1 = 1 // breakpoint 1
    h()
    g(4) {
      val inLambdaG2 = 2 // breakpoint 2
    }
  }
}

inline fun g(gParam: Int, block: () -> Unit) {
  block()
}

inline fun h() {
  val inH = 3
  i()
}

inline fun i() {
  val inI = 4
}
```

How LVT used to look:

```
LocalVariableTable:
Start  Length  Slot  Name   Signature
    20      4     7  $i$f$i   I
    23      1     8  inI$iv$iv   I
    14     11     5  $i$f$h   I
    17      8     6  inH$iv   I
    34      4     7  $i$a$-g-SandboxKt$main$1$1   I
    37      1     8  inLambdaG2   I
    31      9     6  $i$f$g   I
    28     12     5  gParam$iv   I
     8     33     3  $i$a$-g-SandboxKt$main$1   I
    11     30     4  inLambdaG1   I
     6     37     2  $i$f$g   I
     4     39     1  gParam$iv   I
     2     42     0  inMain   I
```

How LVT looks with the new format:
```
LocalVariableTable:
Start  Length  Slot  Name   Signature
   20       4     7  $i$f$i\4\30   I
   23       1     8  inI\4   I
   14      11     5  $i$f$h\3\7   I
   17       8     6  inH\3   I
   34       4     7  $i$a$-g-SandboxKt$main$1$1\6\36\2   I
   37       1     8  inLambdaG2\6   I
   31       9     6  $i$f$g\5\8   I
   28      12     5  gParam\5   I
    8      33     3  $i$a$-g-SandboxKt$main$1\2\28\0   I
   11      30     4  inLambdaG1\2   I
    6      37     2  $i$f$g\1\5   I
    4      39     1  gParam\1   I
    2      42     0  inMain   I
```

Previously the compiler added the `$iv` suffixes to distinguish locals that belong to different inline functions. 
Now we assign scope numbers to marker and local variables. Locals with a scope number belong to the function 
which is represented by a marker variable with the same scope number.

Let’s see how it works in our example:
* `$i$f$g\1\5` has scope number 1, as well as `gParam\1`.
* `$i$a$-g-SandboxKt$main$1\2\28\0` has scope number 2, as well as `inLambdaG1\2`. It also has a surrounding scope number equal to 0. 
   The scope number 0 belongs to the top frame, and basically means that the `inMain` variable should be included in the variables view 
   when we stop at breakpoint 1.
* `$i$f$h\3\7` has scope number 3, as well as `inH\3`.
* `$i$f$i\4\30` has scope number 4, as well as `inI\4`.
* `$i$f$g\5\8` has scope number 5, as well as `gParam\5`.
* `$i$a$-g-SandboxKt$main$1$1\6\36\2` has scope number 6, as well as `inLambdaG2\6`, and surrounding scope number 2, which means that 
   we should include variables that are visible in scope 2 when we stop at breakpoint 2. In our example these variables are `inMain`
   and `inLambdaG1\2`.
