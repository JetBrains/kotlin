interface IBase
object BaseImpl : IBase

interface IOther
fun otherImpl(): IOther = object : IOther {}

class Test1 : IBase by BaseImpl

class Test2 : IBase by BaseImpl, IOther by otherImpl()