package test

var res = 1

class A {

    inline operator fun Int.get(z: Int, p: () -> Int) = this + z + p()

    inline operator fun Int.set(z: Int, p: () -> Int, l: Int) {
        res = this + z + p() + l
    }

}