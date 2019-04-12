/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

function updateSelectedRefs() {
    $("a.ref.selected").removeClass("selected");
    $("a.ref").each(function () {
        if ($(this).attr("jshref") == window.location.href) {
            $(this).addClass("selected");
        }
    });
}

var key = "fir-dump-theme";


var theme = localStorage.getItem(key);
if (!theme) {
    theme = "white"
}

function toggleTheme() {
    var body = $("body");
    body.removeClass(theme);
    if (theme === "white") {
        theme = "dark"
    }
    else {
        theme = "white"
    }
    body.addClass(theme);

    localStorage.setItem(key, theme);
}

$(document).ready(function () {
    $(".fold-container").click(function (event) {
        $(this).toggleClass("unfold");
        event.stopPropagation();
    });
    var refs = $('a.ref:not(.external)');
    refs.each(function () {
        var href = $(this).prop('href');
        $(this).attr('href', 'javascript:void(0);');
        $(this).attr('jshref', href);
    });
    refs.bind('click', function (e) {
        var href = $(this).attr('jshref');
        if (!e.metaKey && e.ctrlKey) {
            e.metaKey = e.ctrlKey;
        }
        if (e.metaKey) {
            location.href = href;
            return false;
        }
    });
    updateSelectedRefs();
    $(window).on('hashchange', function (e) {
        updateSelectedRefs()
    });


    var body = $("body");
    body.prepend("<button class='toggle-theme' onclick='toggleTheme()'>💡</button>");
    body.addClass(theme);


});

window.onload = function () {
    var $line = $('.line');
    var maxWidth = Math.max.apply(Math, $line.map(function () {
        return $(this).width();
    }).get());
    $line.each(function () {
        $(this).width(maxWidth)
    })
};