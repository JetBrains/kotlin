fun main(args : Array<String>) {
    if (<#<condition>#>) {
        <#<block>#>
    } else {
        <#<block>#>
    }

    fun <#<name>#>(<#<params>#>) : <#<returnType>#> {
        <#<body>#>
    }

    for (<#<i>#> in <#<elements>#>) {
        <#<body>#>
    }

    when (<#<expression>#>) {
        <#<condition>#> -> <#<value>#>
        else -> <#<elseValue>#>
    }

    var <#<name>#> = <#<value>#>

    class <#<name>#> {
        <#<body>#>
    }

    class <#<name>#> {
        var <#<name>#> : <#<varType>#>
        get() {
            <#<body>#>
        }
        set(value) {
            <#<body>#>
        }

        val <#<name>#> : <#<valType>#>
        get() {
            <#<body>#>
        }
    }
}

