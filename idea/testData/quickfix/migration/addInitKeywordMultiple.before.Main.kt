// "Add 'init' keyword in whole project" "true"

annotation class Ann1
annotation class Ann2

class A {
    Ann1 Ann2 <caret>{
        class Q {
            {

            }
            Ann2 {

            }
        }
    }
    Ann1 {

    }
}

class B {
    Ann1 Ann2 {
        class Q {
            init {

            }
            Ann2 {

            }
        }
    }
    {

    }
}

