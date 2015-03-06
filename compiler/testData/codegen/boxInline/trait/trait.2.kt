package test

trait InlineTrait {

    inline final fun finalInline(s: () -> String): String {
        return s()
    }

    default object {
        inline final fun finalInline(s: () -> String): String {
            return s()
        }
    }
}

class Z: InlineTrait {

}