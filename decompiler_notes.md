# Заметки по проекту декомпилятора для Kotlin IR

## ВОПРОСЫ

- почему при использовании try/catch как выражения после return у finallyExpression тип - IrTypeOperatorCall?

## TODO-лист

- кастомные геттер и сеттер
- наследование
- super и instance вызовы в конструкторе/методе
- секции инициализации (нужно ли отображать IrInstanceInitializerCall?)
- аргументы по-умолчанию в методах и конструкторах
- extension properties
- вложенные классы
- локальные классы
- companion object, именованный object, top-level object

## LATER

- when с множественным условием через запятую
- return when (varName)
- когда у проперти нет BackingField
- как обращаться к исключению в теле catch
- почему box падает при OK в finally

## DONE

- оператор is, !is в ветке when
- создание экземпляров класса через вызовы конструкторов
- получение свойства экземпляра, инициализированной в конструкторе
- try/catch
- typealias
