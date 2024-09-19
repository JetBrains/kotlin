interface One
interface Two

class Foo <T>(t: T) where T : One, T : <expr>Two</expr> {

}