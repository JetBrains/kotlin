fun foo() {
    @Anno1 val a = @Anno2 1
    val b = @AnonymousFunction fun(@AnonymousParameter a: @AnonymousParameterType Int) {
        @Anno foo()
    }

    @Anno
    fun boo(@Anno a: @Anno Int): @Anno Int {
        @Anno
        fun boo(@Anno a: @Anno Int): @Anno Int {
            @Anno
            fun boo(@Anno a: @Anno Int): @Anno Int {

            }
        }
    }

    @Anno
    class Local : @SuperType SuperClass<@NestedSuperType A<@NestedNestedSuperType B>>(), @SuperInterfaceType SuperInterface<@NestedSuperInterfaceType A<@NestedNestedSuperInterfaceType B>>, @SuperDelegateType SuperInterface<@NestedSuperDelegateType A<@NestedNestedSuperDelegateType B>> by Component {
        @Anno
        class LocalNested : @SuperType SuperClass<@NestedSuperType A<@NestedNestedSuperType B>>(), @SuperInterfaceType SuperInterface<@NestedSuperInterfaceType A<@NestedNestedSuperInterfaceType B>>, @SuperDelegateType SuperInterface<@NestedSuperDelegateType A<@NestedNestedSuperDelegateType B>> by Component {
            @Anno1 val a = @Anno2 1
            val b = @AnonymousFunction fun(@AnonymousParameter a: @AnonymousParameterType Int) {
                @Anno foo()
            }

            @Anno
            fun boo(@Anno a: @Anno Int): @Anno Int {
                @Anno
                fun boo(@Anno a: @Anno Int): @Anno Int {
                    @Anno
                    fun boo(@Anno a: @Anno Int): @Anno Int {

                    }
                }
            }

            @Anno
            class Local : @SuperType SuperClass<@NestedSuperType A<@NestedNestedSuperType B>>(), @SuperInterfaceType SuperInterface<@NestedSuperInterfaceType A<@NestedNestedSuperInterfaceType B>>, @SuperDelegateType SuperInterface<@NestedSuperDelegateType A<@NestedNestedSuperDelegateType B>> by Component {
                @Anno
                class LocalNested : @SuperType SuperClass<@NestedSuperType A<@NestedNestedSuperType B>>(), @SuperInterfaceType SuperInterface<@NestedSuperInterfaceType A<@NestedNestedSuperInterfaceType B>>, @SuperDelegateType SuperInterface<@NestedSuperDelegateType A<@NestedNestedSuperDelegateType B>> by Component {

                }

                @Fun
                fun localMember(): @FunType Int {
                    @Anno1 val a = @Anno2 1
                    val b = @AnonymousFunction fun(@AnonymousParameter a: @AnonymousParameterType Int) {
                        @Anno foo()
                    }

                    @Anno
                    fun boo(@Anno a: @Anno Int): @Anno Int {
                        @Anno
                        fun boo(@Anno a: @Anno Int): @Anno Int {
                            @Anno
                            fun boo(@Anno a: @Anno Int): @Anno Int {

                            }
                        }
                    }

                    @Anno
                    class Local : @SuperType SuperClass<@NestedSuperType A<@NestedNestedSuperType B>>(), @SuperInterfaceType SuperInterface<@NestedSuperInterfaceType A<@NestedNestedSuperInterfaceType B>>, @SuperDelegateType SuperInterface<@NestedSuperDelegateType A<@NestedNestedSuperDelegateType B>> by Component {
                        @Anno
                        class LocalNested : @SuperType SuperClass<@NestedSuperType A<@NestedNestedSuperType B>>(), @SuperInterfaceType SuperInterface<@NestedSuperInterfaceType A<@NestedNestedSuperInterfaceType B>>, @SuperDelegateType SuperInterface<@NestedSuperDelegateType A<@NestedNestedSuperDelegateType B>> by Component {

                        }

                        @Fun
                        fun localMember(): @FunType Int {

                        }

                        @Prop
                        @get:Getter
                        @set:Setter
                        @setparam:Parameter
                        var localProperty: @PropType String = 1
                    }
                }

                @Prop
                @get:Getter
                @set:Setter
                @setparam:Parameter
                var localProperty: @PropType String = 1
            }
        }

        @Fun
        fun localMember(): @FunType Int {
            @Anno1 val a = @Anno2 1
            val b = @AnonymousFunction fun(@AnonymousParameter a: @AnonymousParameterType Int) {
                @Anno foo()
            }

            @Anno
            fun boo(@Anno a: @Anno Int): @Anno Int {
                @Anno
                fun boo(@Anno a: @Anno Int): @Anno Int {
                    @Anno
                    fun boo(@Anno a: @Anno Int): @Anno Int {

                    }
                }
            }

            @Anno
            class Local : @SuperType SuperClass<@NestedSuperType A<@NestedNestedSuperType B>>(), @SuperInterfaceType SuperInterface<@NestedSuperInterfaceType A<@NestedNestedSuperInterfaceType B>>, @SuperDelegateType SuperInterface<@NestedSuperDelegateType A<@NestedNestedSuperDelegateType B>> by Component {
                @Anno
                class LocalNested : @SuperType SuperClass<@NestedSuperType A<@NestedNestedSuperType B>>(), @SuperInterfaceType SuperInterface<@NestedSuperInterfaceType A<@NestedNestedSuperInterfaceType B>>, @SuperDelegateType SuperInterface<@NestedSuperDelegateType A<@NestedNestedSuperDelegateType B>> by Component {

                }

                @Fun
                fun localMember(): @FunType Int {

                }

                val localProperty: @PropType String
                    get() {
                        @Anno1 val a = @Anno2 1
                        val b = @AnonymousFunction fun(@AnonymousParameter a: @AnonymousParameterType Int) {
                            @Anno foo()
                        }

                        @Anno
                        fun boo(@Anno a: @Anno Int): @Anno Int {
                            @Anno
                            fun boo(@Anno a: @Anno Int): @Anno Int {
                                @Anno
                                fun boo(@Anno a: @Anno Int): @Anno Int {

                                }
                            }
                        }

                        @Anno
                        class Local : @SuperType SuperClass<@NestedSuperType A<@NestedNestedSuperType B>>(), @SuperInterfaceType SuperInterface<@NestedSuperInterfaceType A<@NestedNestedSuperInterfaceType B>>, @SuperDelegateType SuperInterface<@NestedSuperDelegateType A<@NestedNestedSuperDelegateType B>> by Component {
                            @Anno
                            class LocalNested : @SuperType SuperClass<@NestedSuperType A<@NestedNestedSuperType B>>(), @SuperInterfaceType SuperInterface<@NestedSuperInterfaceType A<@NestedNestedSuperInterfaceType B>>, @SuperDelegateType SuperInterface<@NestedSuperDelegateType A<@NestedNestedSuperDelegateType B>> by Component {

                            }

                            @Fun
                            fun localMember(): @FunType Int {

                            }

                            @Prop
                            @get:Getter
                            @set:Setter
                            @setparam:Parameter
                            var localProperty: @PropType String = 1
                        }
                    }
            }
        }

        @Prop
        @get:Getter
        @set:Setter
        @setparam:Parameter
        var localProperty: @PropType String = 1
    }
}