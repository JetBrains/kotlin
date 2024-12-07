package test

class Generic1<T>
class Generic1WithBounds<T: Bound1>

class Generic2<A, B>
class Generic2WithBounds<A, B> where A: Bound1, A: Bound2, B: Generic1<A>

class Bound1
interface Bound2

