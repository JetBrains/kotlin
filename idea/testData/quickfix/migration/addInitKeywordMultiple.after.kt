// "Add 'init' keyword in whole project" "true"

annotation class Ann1
annotation class Ann2

class A {
    Ann1 Ann2 init {
        class Q {
            init {

            }
            Ann2 init {

            }
        }
    }
    Ann1 init {

    }
}

class B {
    Ann1 Ann2 init {
        class Q {
            init {

            }
            Ann2 init {

            }
        }
    }
    init {

    }
}

