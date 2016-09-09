fun foo() {
    bar @ ann {
        print(1)
    }

    bar @ {
        print(1)
    }

    bar @ @[ann] {
        print(2)
    }

    bar [] @ {
        print(2)
    }

    bar() @ {
        print(1)
    }

    bar() @[] @ {
        print(2)
    }

    bar(@ {
        param -> param
    })

    bar(@ ann {
        param -> param
    })

    if (true) @ {

    }
    else @[ann] @ ann {

    }
}

// No '}'
val test = {