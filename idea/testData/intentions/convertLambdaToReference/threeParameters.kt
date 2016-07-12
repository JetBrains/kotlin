fun foo(x: Int, y: Int, z: Int) = x - y / z

val x = { a: Int, b: Int, c: Int <caret>-> foo(a, b, c) }