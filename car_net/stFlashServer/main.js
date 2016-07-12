/**
 * Created by user on 7/11/16.
 */

var server = require("./server.js");
var router = require("./router.js");
var handlers = require("./handlers.js");

var handle = {};
handle["/"] = handlers.other;
handle["/loadBin"] = handlers.loadBin;
handle["/other"] = handlers.other;


// UploadR = upRes.UploadResult;
// var res = new UploadR({"error": "test error!"});
// console.log(res.encode());
//
//
// var testBack = UploadR.decode(res.encode());
// console.log(testBack);

// console.log(process.argv);
server.start(router.route, handle);

// var http = require("http");
//
// var global_closure = null;
//
// // nodejs/javascript closures
// http.createServer(function(request, response) {
//     // console.log("test");
//     global_closure && global_closure();
//
//     // console.log(request);
//     // response.writeHead(200, {"Content-Type": "text/plain"});
//     // response.write("Hello World");
//
//     function closure() {
//         console.log(request.toString());
//     }
//     global_closure = closure;
//
//     //writeToJournal().getDBData().processDBData().sendResponse().commitJournal();
//     function writeToJournal() {
//         console.log(request);
//         getDBData(processDBData);
//         getDBData(function() {});
//     }
//
//     function getDBData(callback) {
//         db.getData({ id : request.query['id'] }, onDbDataReady);
//         var i = 0;
//
//         function onDbDataReady(err, someDBData) {
//             if (err) {
//                 return response.error();
//             }
//             console.log(i);
//             callback(someDBData);
//         }
//     }
//
//     function processDBData(dbData) {
//         function sendResponse() {
//
//         }
//     }
//
//     response.end();
// }).listen(8888);
