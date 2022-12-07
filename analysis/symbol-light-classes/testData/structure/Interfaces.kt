package abc

interface SingleInterface

interface BaseInterface1
interface BaseInterface2

interface InterfaceWithBase : BaseInterface1, BaseInterface2

interface InterfaceWithTransitive : InterfaceWithBase
