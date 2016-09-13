# Translator from Kotlin to LLVM

## Fast build
If you want to use the translator without going into any technical details, this section is for you. Using following commands you will be able to compile your Kotlin files, the resulting executable file

    $ make compile files=$(KTFILES) output=$(OUTPUT)

where 
  - $(KTFILES) - list of your Kotlin files separated by spaces
  - $(OUTPUT) - the resulting file
  
  **NOTE:** when you use a fast build, the name of your your entry point function must be **main** and it should not receive arguments

## Building 

For build translator into jar, you must run gradle target

    $ ./gradlew jar
    
**NOTE:** make sure you have jdk version 1.8 or higher


Assembled the translator can be found in the following folder 

    $ cd build/libs/translator

## Using compiler
    $  java -jar $(PATH_TO_TRANSLATOR_JAR) -I $(PATH_TO_KOTLIB) $(KOTLIN_SOURCES)
  where
  - $(PATH_TO_TRANSLATOR_JAR) - path to jar, which you got in building step, by default it `build/libs/translator-1.0.jar`
  - $(PATH_TO_KOTLIB) - path to standard kotlin lib, by default it `./kotstd/include`
  - $(KOTLIN_SOURCES) - the different files that you want to compile
  
### Optional arguments
  - -M - specified entry point of your project, by default it is main
  
    **NOTE:** your entry point function should not receive arguments
  
  - -o - file to redirect output of the generated code
  

## Using generated code

To execute the generated code you should use should link your code with kotlin native runtime. By default, you can find the appropriate library at
`../kotstd/build/stdlib_x86.ll`

If this file is missing you can assemble it yourself

    $  cd ../kotstd && make clean && make

For linking, run the following command

    $  llvm-link-3.6 -S $(KOTLIN_NATIVE_RUNTIME) $(COMPILED_LLVM_CODE)

where $(COMPILED_LLVM_CODE) - code obtained in the step compiler

**NOTE:** The translator supports the llvm version number 3.6. Please make sure you have the correct version of llvm.

## Run

    $  lli-3.6 $(LINKED_CODE)
    
where $(LINKED_CODE)  - code obtained in the previous step

