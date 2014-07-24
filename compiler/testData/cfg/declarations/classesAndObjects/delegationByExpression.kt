trait T

class A(a: Int, b: Int): T

class B(a: Int, b: Int): T by A(a + b, a - b)