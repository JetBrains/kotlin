typealias f = {T<A, B>.T<x>.() : ()}
typealias f = {{(S).() : ()}.() : ()}
typealias f = {{T.() : ()}.() : ()}
typealias f = {{T.T.() : ()}.() : ()}
typealias f = {{T<A, B>.T<x>.() : ()}.() : ()}
typealias f = {{(S).() : ()}.() : ()}

typealias f =  [a] {[a] {(S).() : ()}.() : ()}
typealias f = [a] {[a] {T.() : ()}.() : ()}
typealias f = [a] {[a] {T.T.() : ()}.() : ()}
typealias f = [a] {[a] {T<A, B>.T<x>.() : ()}.() : ()}
typealias f = [a] {[a] {(S).() : ()}.() : ()}