fun foo() {
    bar @ann {
        print(1)
    }

    bar [ann] { // parsed as array access
        print(2)
    }

    bar @ann @[ann] {
        print(2)
    }

    bar() @ann {
        print(1)
    }

    bar() @[ann] {
        print(2)
    }

    bar() @ann @[ann] {
        print(2)
    }

    bar(@ann {
        param -> param
    })

    if (true) @ann {

    }
    else @[ann] @ann {

    }

    foo label@ @ann {

    }
}
