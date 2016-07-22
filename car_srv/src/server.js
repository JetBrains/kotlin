/**
 * Created by user on 7/11/16.
 */
var http = require("http");
var url = require("url");

function start(route, handlers) {
    function onRequest(request, response) {
        var pathname = url.parse(request.url).pathname;
        var httpContent = [];
        request.on("data", function (datas) {
            for (var i = 0; i < datas.length; i++) {
                // httpContent.push(strDatas.charCodeAt(i));
                httpContent.push(datas[i]);
            }
        });

        request.on("end", function () {
            console.log(pathname);
            console.log(httpContent);
            route(handlers, pathname, httpContent, response);
        });
    }
    
    http.createServer(onRequest).listen(8888);
    console.log("Server has started.");
}
exports.start = start;