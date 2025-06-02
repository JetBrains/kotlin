class Clazz
@Anno("str")
context(c:Clazz){}

class Another @Anno("str") context(c:Another) constructor(i: Int)

context(@SharedAnno _: @SharedTypeAnno Clazz)
class OneMore @Anno("str") context(c:Another) constructor(s: String)