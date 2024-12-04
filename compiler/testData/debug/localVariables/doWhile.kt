
// FILE: test.kt
fun shouldContinue(i: Int) = i < 1

fun box() {
  var x = 0
  do {
      var z = 2
      if (shouldContinue(x++)) {
          continue
      }
      // Introduce a variable `y` which is not defined on all control-flow
      // paths to the while condition. Therefore, it should not be in the
      // local variable table at the condition.
      var y = 12
  } while (x < z)
}

// EXPECTATIONS JVM_IR
// test.kt:6 box:
// test.kt:8 box: x:int=0:int
// test.kt:9 box: x:int=0:int, z:int=2:int
// test.kt:3 shouldContinue: i:int=0:int
// test.kt:9 box: x:int=1:int, z:int=2:int
// test.kt:10 box: x:int=1:int, z:int=2:int
// test.kt:16 box: x:int=1:int, z:int=2:int
// test.kt:8 box: x:int=1:int
// test.kt:9 box: x:int=1:int, z:int=2:int
// test.kt:3 shouldContinue: i:int=1:int
// test.kt:9 box: x:int=2:int, z:int=2:int
// test.kt:15 box: x:int=2:int, z:int=2:int
// test.kt:16 box: x:int=2:int, z:int=2:int
// test.kt:17 box: x:int=2:int

// EXPECTATIONS JS_IR
// test.kt:6 box:
// test.kt:8 box: x=0:number
// test.kt:9 box: x=0:number, z=2:number
// test.kt:9 box: x=0:number, z=2:number
// test.kt:9 box: x=1:number, z=2:number
// test.kt:3 shouldContinue: i=0:number
// test.kt:10 box: x=1:number, z=2:number
// test.kt:16 box: x=1:number, z=2:number
// test.kt:8 box: x=1:number, z=2:number
// test.kt:9 box: x=1:number, z=2:number
// test.kt:9 box: x=1:number, z=2:number
// test.kt:9 box: x=2:number, z=2:number
// test.kt:3 shouldContinue: i=1:number
// test.kt:15 box: x=2:number, z=2:number
// test.kt:16 box: x=2:number, z=2:number, y=12:number
// test.kt:17 box: x=2:number, z=2:number, y=12:number
