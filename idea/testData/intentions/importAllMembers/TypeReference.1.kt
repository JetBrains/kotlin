package com.test

sealed class States {
    object Loading : States()
    object Content : States()
    object Error : States()
}