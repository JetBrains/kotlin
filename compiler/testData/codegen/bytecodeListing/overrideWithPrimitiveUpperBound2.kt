open class ATAny<T>(open val x: T)

open class BTChar<T : Char>(override val x: T) : ATAny<T>(x)

class CChar(override val x: Char) : BTChar<Char>('x')
