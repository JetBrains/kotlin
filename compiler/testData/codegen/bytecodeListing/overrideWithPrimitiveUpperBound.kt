open class ATChar<T : Char>(open var x: T)

open class BTChar<T : Char>(override var x: T) : ATChar<T>(x)

class CChar(override var x: Char) : BTChar<Char>('x')
