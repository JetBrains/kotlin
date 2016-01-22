package test

var res = 1

class A {

    inline operator fun Int.get(z: Int, p: () -> Int, defaultt: Int = 100) = this + z + p() + defaultt

    inline operator fun Int.set(z: Int, p: () -> Int, l: Int/*, x : Int = 1000*/) {
        res = this + z + p() + l /*+ x*/
    }
}